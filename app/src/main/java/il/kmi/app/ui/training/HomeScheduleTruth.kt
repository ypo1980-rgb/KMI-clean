package il.kmi.app.ui.training

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
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
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextOverflow
import il.kmi.shared.domain.Belt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.window.Dialog
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
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.graphics.withTranslation
import il.kmi.app.domain.ContentRepo
import il.kmi.app.search.KmiSearchBridge
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiPremiumDropdown
import il.kmi.app.ui.loading.KmiLoadingRings
import il.kmi.app.ui.pdf.KmiPdfDirection
import il.kmi.app.ui.pdf.KmiPdfFooter
import il.kmi.app.ui.pdf.KmiPdfHeader
import il.kmi.app.ui.scaledIconSize
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.kmi.shared.domain.SubTopicRegistry
import il.kmi.shared.questions.model.util.ExerciseTitleFormatter


// ===========================
// Training Summary Palette
// ===========================

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
    isEnglish: Boolean,
    accentColor: Color? = null
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
                                if (accentColor != null) {
                                    Brush.radialGradient(
                                        colors =
                                            listOf(
                                                lerp(
                                                    Color.White,
                                                    accentColor,
                                                    0.35f
                                                ),
                                                accentColor,
                                                lerp(
                                                    accentColor,
                                                    Color.Black,
                                                    0.18f
                                                )
                                            )
                                    )
                                } else {
                                    Brush.radialGradient(
                                        colors =
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
                                    )
                                },
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
                            KmiIconSize.small
                        )
                )
            }
        }
    }
}

@Composable
private fun PremiumSummaryAccentCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val surface =
        MaterialTheme.colorScheme.surface

    val backgroundBrush =
        Brush.verticalGradient(
            colors =
                listOf(
                    lerp(
                        surface,
                        accentColor,
                        0.035f
                    ),
                    surface,
                    lerp(
                        surface,
                        accentColor,
                        0.065f
                    )
                )
        )

    Surface(
        modifier =
            modifier.fillMaxWidth(),
        shape =
            RoundedCornerShape(26.dp),
        color =
            Color.Transparent,
        tonalElevation = 0.dp,
        shadowElevation = 5.dp,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    accentColor.copy(
                        alpha = 0.30f
                    )
            )
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush = backgroundBrush
                    )
        ) {

            /*
             * Glow עליון עדין.
             */
            Box(
                modifier =
                    Modifier
                        .align(
                            Alignment.TopStart
                        )
                        .size(130.dp)
                        .background(
                            brush =
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            accentColor.copy(
                                                alpha = 0.13f
                                            ),
                                            Color.Transparent
                                        )
                                ),
                            shape = CircleShape
                        )
            )

            /*
             * Glow תחתון.
             */
            Box(
                modifier =
                    Modifier
                        .align(
                            Alignment.BottomEnd
                        )
                        .size(170.dp)
                        .background(
                            brush =
                                Brush.radialGradient(
                                    colors =
                                        listOf(
                                            accentColor.copy(
                                                alpha = 0.10f
                                            ),
                                            Color.Transparent
                                        )
                                ),
                            shape = CircleShape
                        )
            )

            /*
             * קו Premium עליון.
             */
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            brush =
                                Brush.horizontalGradient(
                                    colors =
                                        listOf(
                                            Color.Transparent,
                                            accentColor.copy(
                                                alpha = 0.55f
                                            ),
                                            accentColor,
                                            accentColor.copy(
                                                alpha = 0.55f
                                            ),
                                            Color.Transparent
                                        )
                                )
                        )
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 18.dp,
                            end = 18.dp,
                            top = 18.dp,
                            bottom = 16.dp
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        12.dp
                    ),
                content = content
            )
        }
    }
}

@Composable
private fun PremiumSaveRibbon(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .width(46.dp)
                .height(62.dp)
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(
                        bottomStart = 8.dp,
                        bottomEnd = 8.dp
                    ),
                    clip = false
                )
    ) {

        androidx.compose.foundation.Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val notchDepth =
                size.height * 0.18f

            val path =
                Path().apply {
                    moveTo(
                        0f,
                        0f
                    )

                    lineTo(
                        size.width,
                        0f
                    )

                    lineTo(
                        size.width,
                        size.height
                    )

                    lineTo(
                        size.width / 2f,
                        size.height - notchDepth
                    )

                    lineTo(
                        0f,
                        size.height
                    )

                    close()
                }

            drawPath(
                path = path,
                brush =
                    Brush.verticalGradient(
                        colors =
                            listOf(
                                lerp(
                                    accentColor,
                                    Color.White,
                                    0.18f
                                ),
                                accentColor,
                                lerp(
                                    accentColor,
                                    Color.Black,
                                    0.20f
                                )
                            )
                    )
            )
        }

        Box(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 10.dp)
                    .size(28.dp)
                    .background(
                        color =
                            Color.White.copy(
                                alpha = 0.17f
                            ),
                        shape = CircleShape
                    ),
            contentAlignment =
                Alignment.Center
        ) {
            Icon(
                imageVector =
                    Icons.Filled.Check,
                contentDescription =
                    null,
                tint =
                    Color.White,
                modifier =
                    Modifier.size(
                        scaledIconSize(16.dp)
                    )
            )
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
                    },
                    onShare = {
                        shareTrainingSummaryPdf(
                            context = ctx,
                            data =
                                TrainingSummaryPdfData(
                                    dateIso =
                                        state.dateIso,
                                    branchName =
                                        state.branchName,
                                    groupName =
                                        state.groupKey,
                                    coachName =
                                        state.coachName,
                                    generalNotes =
                                        state.notes,
                                    exercises =
                                        state.selected
                                            .values
                                            .sortedBy {
                                                it.name.lowercase()
                                            }
                                            .map { exercise ->
                                                TrainingSummaryPdfExercise(
                                                    name =
                                                        exercise.name,
                                                    highlight =
                                                        exercise.highlight
                                                )
                                            }
                                ),
                            isEnglish = isEnglish
                        )
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
                    .background(
                        brush = kmiScreenBackgroundBrush()
                    )
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
                                            KmiIconSize.small
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

                        val summaryAccent =
                            MaterialTheme
                                .colorScheme
                                .tertiary

                        PremiumSummaryAccentCard(
                            accentColor =
                                summaryAccent
                        ) {

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
                                    Icons.AutoMirrored
                                        .Filled
                                        .Notes,
                                isEnglish =
                                    isEnglish,
                                accentColor =
                                    summaryAccent
                            )

                            Surface(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                color =
                                    summaryAccent.copy(
                                        alpha = 0.16f
                                    ),
                                shape =
                                    RoundedCornerShape(
                                        999.dp
                                    )
                            ) {
                                Spacer(
                                    Modifier.height(
                                        2.dp
                                    )
                                )
                            }

                            Surface(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                shape =
                                    RoundedCornerShape(16.dp),
                                color =
                                    lerp(
                                        MaterialTheme
                                            .colorScheme
                                            .surface,
                                        summaryAccent,
                                        0.055f
                                    ),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                border =
                                    BorderStroke(
                                        width = 1.dp,
                                        color =
                                            summaryAccent.copy(
                                                alpha = 0.22f
                                            )
                                    )
                            ) {

                                Box(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                ) {

                                    /*
                                     * Glow דקורטיבי בתוך אזור הטקסט.
                                     */
                                    Box(
                                        modifier =
                                            Modifier
                                                .align(
                                                    Alignment.BottomStart
                                                )
                                                .size(150.dp)
                                                .background(
                                                    brush =
                                                        Brush.radialGradient(
                                                            colors =
                                                                listOf(
                                                                    summaryAccent.copy(
                                                                        alpha = 0.09f
                                                                    ),
                                                                    Color.Transparent
                                                                )
                                                        ),
                                                    shape = CircleShape
                                                )
                                    )

                                    OutlinedTextField(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .heightIn(
                                                    min = 145.dp
                                                ),
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
                                                    KmiTypography.caption,
                                                color =
                                                    summaryAccent
                                            )
                                        },
                                        minLines = 5,
                                        textStyle =
                                            KmiTypography.body.copy(
                                                color =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .onSurface,
                                                fontWeight =
                                                    FontWeight.SemiBold
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
                                                    summaryAccent.copy(
                                                        alpha = 0.50f
                                                    ),

                                                unfocusedBorderColor =
                                                    Color.Transparent,

                                                focusedLabelColor =
                                                    summaryAccent,

                                                unfocusedLabelColor =
                                                    summaryAccent,

                                                cursorColor =
                                                    summaryAccent,

                                                focusedContainerColor =
                                                    Color.Transparent,

                                                unfocusedContainerColor =
                                                    Color.Transparent
                                            )
                                    )
                                }
                            }
                        }
                    }
// -----------------------------
// שמירה
// -----------------------------
                    item {

                        val saveAccent =
                            MaterialTheme
                                .colorScheme
                                .primary

                        Box(
                            modifier =
                                Modifier.fillMaxWidth()
                        ) {

                            PremiumSummaryAccentCard(
                                accentColor =
                                    saveAccent
                            ) {

                                /*
                                 * מרווח קטן עבור ה־Ribbon.
                                 */
                                Spacer(
                                    Modifier.height(6.dp)
                                )

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
                                    isEnglish,
                                accentColor =
                                    saveAccent
                            )

                            Surface(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                color =
                                    saveAccent.copy(
                                        alpha = 0.16f
                                    ),
                                shape =
                                    RoundedCornerShape(
                                        999.dp
                                    )
                            ) {
                                Spacer(
                                    Modifier.height(
                                        2.dp
                                    )
                                )
                            }

                            Surface(
                                modifier =
                                    Modifier.fillMaxWidth(),
                                shape =
                                    RoundedCornerShape(
                                        20.dp
                                    ),
                                color =
                                    lerp(
                                        MaterialTheme
                                            .colorScheme
                                            .surface,
                                        saveAccent,
                                        0.07f
                                    ),
                                tonalElevation = 0.dp,
                                shadowElevation = 0.dp,
                                border =
                                    BorderStroke(
                                        width = 1.dp,
                                        color =
                                            saveAccent.copy(
                                                alpha = 0.18f
                                            )
                                    )
                            ) {

                                val saveButtonEnabled =
                                    !state.isSaving

                                val saveButtonBrush =
                                    Brush.horizontalGradient(
                                        colors =
                                            listOf(
                                                lerp(
                                                    saveAccent,
                                                    Color.Black,
                                                    0.18f
                                                ),
                                                saveAccent,
                                                lerp(
                                                    saveAccent,
                                                    Color.White,
                                                    0.16f
                                                ),
                                                saveAccent
                                            )
                                    )

                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 10.dp,
                                                vertical = 10.dp
                                            )
                                            .height(58.dp)
                                            .shadow(
                                                elevation = 7.dp,
                                                shape =
                                                    RoundedCornerShape(
                                                        999.dp
                                                    ),
                                                clip = false
                                            )
                                            .clip(
                                                RoundedCornerShape(
                                                    999.dp
                                                )
                                            )
                                            .background(
                                                brush =
                                                    if (saveButtonEnabled) {
                                                        saveButtonBrush
                                                    } else {
                                                        Brush.horizontalGradient(
                                                            colors =
                                                                listOf(
                                                                    saveAccent.copy(
                                                                        alpha = 0.35f
                                                                    ),
                                                                    saveAccent.copy(
                                                                        alpha = 0.45f
                                                                    )
                                                                )
                                                        )
                                                    }
                                            )
                                            .clickable(
                                                enabled =
                                                    saveButtonEnabled
                                            ) {

                                                val key =
                                                    "training_summary_days"

                                                val cleanIso =
                                                    state
                                                        .dateIso
                                                        .trim()
                                                        .take(10)

                                                fun markSummaryDayLocally() {

                                                    if (
                                                        cleanIso.isBlank()
                                                    ) {
                                                        return
                                                    }

                                                    val summaryCur =
                                                        summarySp
                                                            .getStringSet(
                                                                key,
                                                                emptySet()
                                                            )
                                                            ?.toMutableSet()
                                                            ?: mutableSetOf()

                                                    summaryCur.add(
                                                        cleanIso
                                                    )

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

                                                        /*
                                                         * רק סיכום שנשמר בפועל
                                                         * מסומן בלוח.
                                                         */
                                                        markSummaryDayLocally()

                                                        onBack
                                                            ?.invoke()
                                                    },

                                                    onError = {

                                                        /*
                                                         * נשארים במסך במקרה
                                                         * של כשל שמירה.
                                                         */
                                                    }
                                                )
                                            },
                                    contentAlignment =
                                        Alignment.Center
                                ) {

                                    /*
                                     * Highlight עליון עדין.
                                     */
                                    Box(
                                        modifier =
                                            Modifier
                                                .align(
                                                    Alignment.TopCenter
                                                )
                                                .fillMaxWidth()
                                                .height(1.dp)
                                                .background(
                                                    Color.White.copy(
                                                        alpha = 0.45f
                                                    )
                                                )
                                    )

                                    Row(
                                        verticalAlignment =
                                            Alignment.CenterVertically,
                                        horizontalArrangement =
                                            Arrangement.Center
                                    ) {

                                        /*
                                         * עיגול האייקון.
                                         */
                                        Box(
                                            modifier =
                                                Modifier
                                                    .size(34.dp)
                                                    .background(
                                                        color =
                                                            Color.White.copy(
                                                                alpha = 0.16f
                                                            ),
                                                        shape =
                                                            CircleShape
                                                    ),
                                            contentAlignment =
                                                Alignment.Center
                                        ) {

                                            Icon(
                                                imageVector =
                                                    Icons.Filled.Check,
                                                contentDescription =
                                                    null,
                                                tint =
                                                    Color.White,
                                                modifier =
                                                    Modifier.size(
                                                        scaledIconSize(
                                                            17.dp
                                                        )
                                                    )
                                            )
                                        }

                                        Spacer(
                                            Modifier.width(
                                                10.dp
                                            )
                                        )

                                        Text(
                                            text =
                                                if (
                                                    state.isSaving
                                                ) {
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
                                                KmiTypography
                                                    .action
                                                    .copy(
                                                        fontWeight =
                                                            FontWeight.ExtraBold
                                                    ),
                                            color =
                                                Color.White,
                                            maxLines = 1,
                                            textAlign =
                                                TextAlign.Center
                                        )
                                    }
                                }
                            }

                                Box(
                                    modifier =
                                        Modifier.fillMaxWidth()
                                ) {
                                    PremiumSaveRibbon(
                                        accentColor =
                                            saveAccent,
                                        modifier =
                                            Modifier
                                                .align(
                                                    if (isEnglish) {
                                                        Alignment.TopStart
                                                    } else {
                                                        Alignment.TopEnd
                                                    }
                                                )
                                                .offset(
                                                    x =
                                                        if (isEnglish) {
                                                            18.dp
                                                        } else {
                                                            (-18).dp
                                                        },
                                                    y = (-1).dp
                                                )
                                    )
                                }
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
                        onDismiss = {
                            showAddExercisesSheet = false
                        }
                    )
                }

                if (state.isSaving) {
                    Dialog(
                        onDismissRequest = {}
                    ) {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .surface,
                            tonalElevation = 0.dp,
                            shadowElevation = 0.dp,
                            border = BorderStroke(
                                width = 1.dp,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .outlineVariant
                            )
                        ) {
                            KmiLoadingRings(
                                modifier =
                                    Modifier.padding(
                                        horizontal = 32.dp,
                                        vertical = 24.dp
                                    ),
                                text =
                                    tr(
                                        "שומר את סיכום האימון...",
                                        "Saving training summary..."
                                    )
                            )
                        }
                    }
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
            modifier =
                Modifier.fillMaxWidth()
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
                                            KmiIconSize.small
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
                                    scaledIconSize(16.dp)
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
                                scaledIconSize(15.dp)
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

private fun exerciseBeltFromId(
    exerciseId: String
): Belt? {

    val beltId =
        exerciseId
            .substringBefore("|")
            .trim()

    if (beltId.isBlank()) {
        return null
    }

    return Belt.entries
        .firstOrNull { belt ->
            belt.id.equals(
                beltId,
                ignoreCase = true
            )
        }
}

@Composable
private fun SelectedExerciseEditor(
    item: SelectedExerciseUi,
    onRemove: () -> Unit,
    onHighlight: (String) -> Unit
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
                textDirection =
                    TextDirection.Ltr
            )
        } else {
            TextStyle(
                textDirection =
                    TextDirection.Rtl
            )
        }

    val belt =
        remember(item.exerciseId) {
            exerciseBeltFromId(
                item.exerciseId
            )
        }

    val beltAccentColor =
        when (belt) {

            Belt.WHITE ->
                Color(0xFFD1D5DB)

            Belt.YELLOW ->
                Color(0xFFFACC15)

            Belt.ORANGE ->
                Color(0xFFF97316)

            Belt.GREEN ->
                Color(0xFF22C55E)

            Belt.BLUE ->
                Color(0xFF3B82F6)

            Belt.BROWN ->
                Color(0xFF8B5A2B)

            Belt.BLACK ->
                Color(0xFF111111)

            null ->
                MaterialTheme
                    .colorScheme
                    .primary
        }

    val beltLabel =
        when {
            belt == null ->
                ""

            isEnglish ->
                when (belt) {
                    Belt.WHITE -> "White belt"
                    Belt.YELLOW -> "Yellow belt"
                    Belt.ORANGE -> "Orange belt"
                    Belt.GREEN -> "Green belt"
                    Belt.BLUE -> "Blue belt"
                    Belt.BROWN -> "Brown belt"
                    Belt.BLACK -> "Black belt"
                }

            else ->
                belt.heb
        }

    /*
     * אותו רעיון עיצובי של ExerciseExplanationDialog:
     * בסיס בהיר + נגיעה עדינה מצבע החגורה.
     */
    val cardBrush =
        Brush.verticalGradient(
            colors =
                listOf(
                    MaterialTheme
                        .colorScheme
                        .surface,

                    lerp(
                        MaterialTheme
                            .colorScheme
                            .surface,
                        beltAccentColor,
                        0.10f
                    ),

                    lerp(
                        MaterialTheme
                            .colorScheme
                            .surface,
                        beltAccentColor,
                        0.05f
                    ),

                    MaterialTheme
                        .colorScheme
                        .surface
                )
        )

    val innerChipColor =
        lerp(
            MaterialTheme
                .colorScheme
                .surface,
            beltAccentColor,
            0.08f
        )

    var notesOpen by
    rememberSaveable(
        item.exerciseId
    ) {
        mutableStateOf(
            item.highlight.isNotBlank()
        )
    }

    Surface(
        modifier =
            Modifier
                .fillMaxWidth(),
        shape =
            RoundedCornerShape(
                24.dp
            ),
        color =
            Color.Transparent,
        tonalElevation =
            0.dp,
        shadowElevation =
            0.dp,
        border =
            BorderStroke(
                width = 1.dp,
                color =
                    beltAccentColor
                        .copy(
                            alpha = 0.34f
                        )
            )
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(
                        brush =
                            cardBrush
                    )
        ) {

            /*
             * פס חגורה מודגש בצד הכרטיס.
             */
            Box(
                modifier =
                    Modifier
                        .align(
                            if (isEnglish) {
                                Alignment.CenterStart
                            } else {
                                Alignment.CenterEnd
                            }
                        )
                        .width(6.dp)
                        .fillMaxSize()
                        .background(
                            color =
                                beltAccentColor
                        )
            )

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start =
                                if (isEnglish) {
                                    18.dp
                                } else {
                                    14.dp
                                },
                            end =
                                if (isEnglish) {
                                    14.dp
                                } else {
                                    18.dp
                                },
                            top = 14.dp,
                            bottom = 12.dp
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(
                        10.dp
                    )
            ) {

                Text(
                    text =
                        item.name,
                    style =
                        KmiTypography
                            .cardTitle
                            .merge(
                                titleDirectionStyle
                            ),
                    fontWeight =
                        FontWeight.ExtraBold,
                    color =
                        SummaryTextDark,
                    textAlign =
                        textAlignPrimary,
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    maxLines = 3,
                    overflow =
                        TextOverflow.Ellipsis
                )

                if (
                    beltLabel.isNotBlank()
                ) {

                    Text(
                        text =
                            beltLabel,
                        style =
                            KmiTypography
                                .secondary
                                .merge(
                                    titleDirectionStyle
                                ),
                        fontWeight =
                            FontWeight.ExtraBold,
                        color =
                            beltAccentColor,
                        textAlign =
                            textAlignPrimary,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                    )
                }

                if (
                    item.topic.isNotBlank()
                ) {

                    Surface(
                        shape =
                            RoundedCornerShape(
                                999.dp
                            ),
                        color =
                            innerChipColor,
                        tonalElevation =
                            0.dp,
                        shadowElevation =
                            0.dp,
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color =
                                    beltAccentColor
                                        .copy(
                                            alpha = 0.20f
                                        )
                            )
                    ) {

                        Text(
                            text =
                                item.topic,
                            style =
                                KmiTypography
                                    .secondary,
                            color =
                                SummaryTextDark
                                    .copy(
                                        alpha = 0.86f
                                    ),
                            modifier =
                                Modifier
                                    .padding(
                                        horizontal =
                                            12.dp,
                                        vertical =
                                            6.dp
                                    )
                        )
                    }
                }

                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    horizontalArrangement =
                        if (isEnglish) {
                            Arrangement.Start
                        } else {
                            Arrangement.End
                        },
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    FilledTonalIconButton(
                        onClick = {
                            notesOpen =
                                !notesOpen
                        },
                        colors =
                            IconButtonDefaults
                                .filledTonalIconButtonColors(
                                    containerColor =
                                        beltAccentColor
                                            .copy(
                                                alpha = 0.14f
                                            ),
                                    contentColor =
                                        SummaryTextDark
                                )
                    ) {

                        Icon(
                            imageVector =
                                Icons.Filled.EditNote,
                            contentDescription =
                                if (notesOpen) {
                                    tr(
                                        "סגור הערות",
                                        "Close notes"
                                    )
                                } else {
                                    tr(
                                        "פתח הערות",
                                        "Open notes"
                                    )
                                }
                        )
                    }

                    Spacer(
                        Modifier.width(
                            8.dp
                        )
                    )

                    OutlinedIconButton(
                        onClick =
                            onRemove,
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .error
                                        .copy(
                                            alpha = 0.55f
                                        )
                            )
                    ) {

                        Icon(
                            imageVector =
                                Icons.Filled.Delete,
                            contentDescription =
                                tr(
                                    "מחק תרגיל",
                                    "Delete exercise"
                                ),
                            tint =
                                MaterialTheme
                                    .colorScheme
                                    .error
                        )
                    }
                }

                Surface(
                    modifier =
                        Modifier
                            .fillMaxWidth(),
                    color =
                        beltAccentColor
                            .copy(
                                alpha = 0.18f
                            ),
                    shape =
                        RoundedCornerShape(
                            999.dp
                        )
                ) {

                    Spacer(
                        Modifier.height(
                            1.5.dp
                        )
                    )
                }

                if (
                    !notesOpen &&
                    item.highlight.isNotBlank()
                ) {

                    Surface(
                        modifier =
                            Modifier
                                .fillMaxWidth(),
                        shape =
                            RoundedCornerShape(
                                16.dp
                            ),
                        color =
                            innerChipColor,
                        tonalElevation =
                            0.dp,
                        shadowElevation =
                            0.dp,
                        border =
                            BorderStroke(
                                width = 1.dp,
                                color =
                                    beltAccentColor
                                        .copy(
                                            alpha = 0.18f
                                        )
                            )
                    ) {

                        Text(
                            text =
                                item.highlight,
                            style =
                                KmiTypography.body,
                            color =
                                SummaryTextDark,
                            textAlign =
                                textAlignPrimary,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(
                                        horizontal =
                                            12.dp,
                                        vertical =
                                            10.dp
                                    ),
                            maxLines = 3,
                            overflow =
                                TextOverflow.Ellipsis
                        )
                    }
                }

                if (
                    notesOpen
                ) {

                    OutlinedTextField(
                        modifier =
                            Modifier
                                .fillMaxWidth(),
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
                                    KmiTypography
                                        .caption
                            )
                        },
                        minLines = 3,
                        textStyle =
                            KmiTypography
                                .body
                                .copy(
                                    color =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface
                                ),
                        colors =
                            OutlinedTextFieldDefaults
                                .colors(
                                    focusedTextColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface,

                                    unfocusedTextColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurface,

                                    focusedBorderColor =
                                        beltAccentColor,

                                    unfocusedBorderColor =
                                        beltAccentColor
                                            .copy(
                                                alpha =
                                                    0.32f
                                            ),

                                    focusedLabelColor =
                                        beltAccentColor,

                                    unfocusedLabelColor =
                                        MaterialTheme
                                            .colorScheme
                                            .onSurfaceVariant,

                                    cursorColor =
                                        beltAccentColor,

                                    focusedContainerColor =
                                        innerChipColor,

                                    unfocusedContainerColor =
                                        innerChipColor
                                )
                    )
                }
            }
        }
    }
}

// =========================
// Training summary PDF
// =========================

private data class TrainingSummaryPdfExercise(
    val name: String,
    val highlight: String
)

private data class TrainingSummaryPdfData(
    val dateIso: String,
    val branchName: String,
    val groupName: String,
    val coachName: String,
    val generalNotes: String,
    val exercises: List<TrainingSummaryPdfExercise>
)

@Suppress("SpellCheckingInspection")
private fun shareTrainingSummaryPdf(
    context: Context,
    data: TrainingSummaryPdfData,
    isEnglish: Boolean
) {
    runCatching {
        val pdfFile =
            createTrainingSummaryPdf(
                context = context,
                data = data,
                isEnglish = isEnglish
            )

        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

        val shareIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"

                putExtra(
                    Intent.EXTRA_SUBJECT,
                    if (isEnglish) {
                        "KMI training summary"
                    } else {
                        "סיכום אימון ק.מ.י"
                    }
                )

                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

        context.startActivity(
            Intent.createChooser(
                shareIntent,
                if (isEnglish) {
                    "Share training summary PDF"
                } else {
                    "שיתוף סיכום האימון כ־PDF"
                }
            )
        )
    }.onFailure {
        Toast.makeText(
            context,
            if (isEnglish) {
                "The PDF file could not be created"
            } else {
                "לא ניתן היה ליצור את קובץ ה־PDF"
            },
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun createTrainingSummaryPdf(
    context: Context,
    data: TrainingSummaryPdfData,
    isEnglish: Boolean
): File {
    val pageWidth = 595
    val pageHeight = 842
    val horizontalMargin = 40f
    val contentWidth =
        (pageWidth - horizontalMargin * 2f)
            .toInt()

    val contentBottom =
        pageHeight -
                KmiPdfFooter.CONTENT_BOTTOM_PADDING

    val document =
        PdfDocument()

    val regularTypeface =
        Typeface.create(
            Typeface.SANS_SERIF,
            Typeface.NORMAL
        )

    val boldTypeface =
        Typeface.create(
            Typeface.SANS_SERIF,
            Typeface.BOLD
        )

    fun textPaint(
        size: Float,
        color: Int,
        bold: Boolean = false
    ): TextPaint {
        return TextPaint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            textSize = size
            this.color = color
            typeface =
                if (bold) {
                    boldTypeface
                } else {
                    regularTypeface
                }
        }
    }

    val titleColor =
        android.graphics.Color.rgb(
            6,
            43,
            74
        )

    val textColor =
        android.graphics.Color.rgb(
            23,
            32,
            51
        )

    val mutedColor =
        android.graphics.Color.rgb(
            71,
            84,
            103
        )

    val dividerColor =
        android.graphics.Color.rgb(
            213,
            222,
            229
        )

    val sectionPaint =
        textPaint(
            size = 17f,
            color = titleColor,
            bold = true
        )

    val bodyPaint =
        textPaint(
            size = 13f,
            color = textColor
        )

    val emphasizedPaint =
        textPaint(
            size = 13f,
            color = textColor,
            bold = true
        )

    val mutedPaint =
        textPaint(
            size = 12f,
            color = mutedColor
        )

    val linePaint =
        Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            color = dividerColor
            strokeWidth = 1.2f
        }

    fun tr(
        he: String,
        en: String
    ): String {
        return if (isEnglish) {
            en
        } else {
            he
        }
    }

    fun cleanText(
        value: String,
        fallback: String
    ): String {
        return value
            .trim()
            .ifBlank {
                fallback
            }
    }

    val headerSubtitle =
        listOf(
            data.dateIso.trim(),
            data.branchName.trim(),
            data.groupName.trim()
        )
            .filter {
                it.isNotBlank()
            }
            .joinToString(" · ")

    var pageNumber = 0
    var pageStarted = false
    lateinit var page: PdfDocument.Page
    lateinit var canvas: android.graphics.Canvas
    var y = KmiPdfHeader.CONTENT_TOP

    fun drawHeader() {
        KmiPdfHeader.draw(
            context = context,
            canvas = canvas,
            pageWidth = pageWidth,
            isEnglish = isEnglish,
            titleHebrew = "סיכום אימון",
            titleEnglish = "Training Summary",
            subtitleHebrew = headerSubtitle,
            subtitleEnglish = headerSubtitle
        )

        y = KmiPdfHeader.CONTENT_TOP
    }

    fun drawFooter() {
        KmiPdfFooter.draw(
            canvas = canvas,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            pageNumber = pageNumber,
            totalPages = null,
            isEnglish = isEnglish
        )
    }

    fun startPage() {
        if (pageStarted) {
            drawFooter()
            document.finishPage(page)
        }

        pageNumber++

        page =
            document.startPage(
                PdfDocument.PageInfo
                    .Builder(
                        pageWidth,
                        pageHeight,
                        pageNumber
                    )
                    .create()
            )

        pageStarted = true
        canvas = page.canvas
        drawHeader()
    }

    fun ensureSpace(
        requiredHeight: Float
    ) {
        if (
            y + requiredHeight >
            contentBottom
        ) {
            startPage()
        }
    }

    fun wrappedLines(
        text: String,
        paint: TextPaint
    ): List<String> {
        val result =
            mutableListOf<String>()

        text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .split('\n')
            .forEach { paragraph ->
                if (paragraph.isBlank()) {
                    result.add("")
                    return@forEach
                }

                var currentLine = ""

                paragraph
                    .trim()
                    .split(
                        Regex("\\s+")
                    )
                    .forEach { word ->
                        val candidate =
                            if (currentLine.isBlank()) {
                                word
                            } else {
                                "$currentLine $word"
                            }

                        if (
                            paint.measureText(candidate) <=
                            contentWidth
                        ) {
                            currentLine = candidate
                        } else {
                            if (currentLine.isNotBlank()) {
                                result.add(currentLine)
                            }

                            currentLine = word
                        }
                    }

                if (currentLine.isNotBlank()) {
                    result.add(currentLine)
                }
            }

        return result
    }

    fun drawTextLines(
        text: String,
        paint: TextPaint,
        bottomSpacing: Float = 6f
    ) {
        val lines =
            wrappedLines(
                text = text,
                paint = paint
            )

        lines.forEach { line ->
            val displayLine =
                line.ifBlank {
                    " "
                }

            val layout =
                StaticLayout.Builder
                    .obtain(
                        displayLine,
                        0,
                        displayLine.length,
                        paint,
                        contentWidth
                    )
                    .setAlignment(
                        KmiPdfDirection.layoutAlignment(
                            isEnglish = isEnglish
                        )
                    )
                    .setTextDirection(
                        KmiPdfDirection.textDirection(
                            isEnglish = isEnglish
                        )
                    )
                    .setIncludePad(false)
                    .setMaxLines(1)
                    .build()

            ensureSpace(
                layout.height + 4f
            )

            canvas.withTranslation(
                x = horizontalMargin,
                y = y
            ) {
                layout.draw(this)
            }

            y += layout.height + 4f
        }

        y += bottomSpacing
    }

    fun drawSectionTitle(
        title: String
    ) {
        ensureSpace(34f)

        drawTextLines(
            text = title,
            paint = sectionPaint,
            bottomSpacing = 4f
        )

        canvas.drawLine(
            horizontalMargin,
            y,
            pageWidth - horizontalMargin,
            y,
            linePaint
        )

        y += 12f
    }

    fun drawField(
        label: String,
        value: String
    ) {
        drawTextLines(
            text = label,
            paint = emphasizedPaint,
            bottomSpacing = 1f
        )

        drawTextLines(
            text = value,
            paint = bodyPaint,
            bottomSpacing = 8f
        )
    }

    startPage()

    drawSectionTitle(
        tr(
            "פרטי האימון",
            "Training details"
        )
    )

    drawField(
        label =
            tr(
                "תאריך",
                "Date"
            ),
        value =
            cleanText(
                data.dateIso,
                tr(
                    "לא נבחר תאריך",
                    "No date selected"
                )
            )
    )

    drawField(
        label =
            tr(
                "סניף",
                "Branch"
            ),
        value =
            cleanText(
                data.branchName,
                tr(
                    "לא צוין סניף",
                    "No branch specified"
                )
            )
    )

    drawField(
        label =
            tr(
                "קבוצה",
                "Group"
            ),
        value =
            cleanText(
                data.groupName,
                tr(
                    "לא צוינה קבוצה",
                    "No group specified"
                )
            )
    )

    drawField(
        label =
            tr(
                "מאמן",
                "Coach"
            ),
        value =
            cleanText(
                data.coachName,
                tr(
                    "לא צוין מאמן",
                    "No coach specified"
                )
            )
    )

    drawSectionTitle(
        tr(
            "תרגילים שבוצעו באימון",
            "Exercises performed in training"
        )
    )

    if (data.exercises.isEmpty()) {
        drawTextLines(
            text =
                tr(
                    "לא נוספו תרגילים לסיכום האימון.",
                    "No exercises were added to the training summary."
                ),
            paint = mutedPaint,
            bottomSpacing = 12f
        )
    } else {
        data.exercises
            .forEachIndexed { index, exercise ->
                ensureSpace(48f)

                drawTextLines(
                    text =
                        "${index + 1}. ${
                            cleanText(
                                exercise.name,
                                tr(
                                    "תרגיל ללא שם",
                                    "Unnamed exercise"
                                )
                            )
                        }",
                    paint = emphasizedPaint,
                    bottomSpacing = 2f
                )

                if (exercise.highlight.isNotBlank()) {
                    drawTextLines(
                        text =
                            tr(
                                "דגשים: ",
                                "Highlights: "
                            ) +
                                    exercise.highlight.trim(),
                        paint = mutedPaint,
                        bottomSpacing = 8f
                    )
                } else {
                    y += 5f
                }
            }
    }

    drawSectionTitle(
        tr(
            "סיכום כללי",
            "General summary"
        )
    )

    drawTextLines(
        text =
            cleanText(
                data.generalNotes,
                tr(
                    "לא נוסף סיכום כללי.",
                    "No general summary was added."
                )
            ),
        paint = bodyPaint,
        bottomSpacing = 8f
    )

    drawFooter()
    document.finishPage(page)

    val reportsDirectory =
        File(
            context.cacheDir,
            "reports"
        ).apply {
            mkdirs()
        }

    val safeDate =
        data.dateIso
            .trim()
            .replace(
                Regex("[^0-9A-Za-zא-ת_-]"),
                "_"
            )
            .ifBlank {
                "training"
            }

    val fileName =
        if (isEnglish) {
            "KMI_Training_Summary_$safeDate.pdf"
        } else {
            "סיכום_אימון_קמי_$safeDate.pdf"
        }

    val outputFile =
        File(
            reportsDirectory,
            fileName
        )

    FileOutputStream(
        outputFile
    ).use { output ->
        document.writeTo(output)
    }

    document.close()

    return outputFile
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