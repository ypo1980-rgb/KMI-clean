package il.kmi.app.screens.coach.statistics

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import il.kmi.app.ui.DrawerBridge
import il.kmi.app.ui.KmiIconSize
import il.kmi.app.ui.KmiTopBar
import il.kmi.app.ui.KmiTypography
import il.kmi.app.ui.loading.KmiLoadingRings
import il.yuval.ui.theme.kmiScreenBackgroundBrush
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


//================================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NationalStatisticsScreen(
    isEnglish: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
    shareTrigger: Int = 0,
    onOpenDrawer: () -> Unit = {
        DrawerBridge.open()
    },
    onOpenHome: () -> Unit = onBack,
    repository: NationalStatisticsRepository =
        remember { NationalStatisticsRepository() }
) {
    var allRecords by remember {
        mutableStateOf<List<NationalTraineeRecord>>(emptyList())
    }

    var filters by remember {
        mutableStateOf(NationalStatisticsFilters())
    }

    var isLoading by remember {
        mutableStateOf(true)
    }

    var errorMessage by remember {
        mutableStateOf<String?>(null)
    }

    var reloadKey by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(reloadKey) {
        isLoading = true
        errorMessage = null

        when (val result = repository.loadAllTrainees()) {
            is NationalStatisticsLoadResult.Success -> {
                allRecords = result.records
                isLoading = false
            }

            is NationalStatisticsLoadResult.Error -> {
                allRecords = emptyList()
                errorMessage = if (isEnglish) {
                    result.messageEn
                } else {
                    result.messageHe
                }
                isLoading = false
            }
        }
    }

    val snapshot = remember(allRecords, filters) {
        NationalStatisticsCalculator.calculate(
            allRecords = allRecords,
            filters = filters
        )
    }

    val context = LocalContext.current

    LaunchedEffect(shareTrigger) {
        if (
            shareTrigger > 0 &&
            !isLoading &&
            errorMessage == null
        ) {
            shareNationalStatisticsPdf(
                context = context,
                snapshot = snapshot,
                filters = filters,
                isEnglish = isEnglish
            )
        }
    }

    val availableBranches = remember(allRecords) {
        allRecords
            .asSequence()
            .flatMap { it.branches.asSequence() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }

    val availableGroups = remember(allRecords) {
        allRecords
            .asSequence()
            .flatMap { it.groups.asSequence() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
            .toList()
    }

    val availableBelts = remember(allRecords) {
        allRecords
            .map { it.belt.trim().ifBlank { "ללא דרגה" } }
            .distinct()
            .sortedWith(
                compareBy { beltOrder(it) }
            )
    }

    val screenDirection = if (isEnglish) {
        LayoutDirection.Ltr
    } else {
        LayoutDirection.Rtl
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides screenDirection
    ) {
        Scaffold(
            modifier = modifier,
            topBar = {
                /*
                 * כאשר המסך מוצג בתוך הטאב, הכותרת
                 * הגלובלית כבר מוצגת על ידי המסך המארח.
                 */
                if (!embedded) {
                    val topBarContext =
                        LocalContext.current

                    val topBarLanguageManager =
                        remember(topBarContext) {
                            AppLanguageManager(
                                topBarContext
                            )
                        }

                    KmiTopBar(
                        title = tr(
                            isEnglish,
                            "סטטיסטיקה",
                            "Statistics"
                        ),
                        onOpenDrawer = onOpenDrawer,
                        onHome = onOpenHome,
                        showTopShare = false,
                        onShare = {},
                        showTopHome = false,
                        showRoleStatus = false,
                        lockSearch = false,
                        showBottomActions = true,

                        currentLang =
                            if (
                                topBarLanguageManager
                                    .getCurrentLanguage() ==
                                AppLanguage.ENGLISH
                            ) {
                                "en"
                            } else {
                                "he"
                            },

                        onToggleLanguage = {
                            val newLanguage =
                                if (
                                    topBarLanguageManager
                                        .getCurrentLanguage() ==
                                    AppLanguage.HEBREW
                                ) {
                                    AppLanguage.ENGLISH
                                } else {
                                    AppLanguage.HEBREW
                                }

                            topBarLanguageManager
                                .setLanguage(
                                    newLanguage
                                )

                            (topBarContext as? Activity)
                                ?.recreate()
                        }
                    )
                }
            },
            containerColor = Color.Transparent
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = kmiScreenBackgroundBrush()
                    )
                    .padding(innerPadding)
            ) {
                when {
                    isLoading -> {
                        NationalStatisticsLoading(
                            isEnglish = isEnglish
                        )
                    }

                    errorMessage != null -> {
                        NationalStatisticsError(
                            message = errorMessage.orEmpty(),
                            isEnglish = isEnglish,
                            onRetry = {
                                reloadKey += 1
                            }
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                            contentPadding = PaddingValues(
                                start = 14.dp,
                                end = 14.dp,
                                top = 14.dp,
                                bottom = 32.dp
                            ),
                            verticalArrangement =
                                Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                NationalStatisticsHero(
                                    snapshot = snapshot,
                                    isEnglish = isEnglish
                                )
                            }

                            item {
                                NationalStatisticsSearch(
                                    query = filters.searchQuery,
                                    isEnglish = isEnglish,
                                    onQueryChange = { query ->
                                        filters = filters.copy(
                                            searchQuery = query
                                        )
                                    }
                                )
                            }

                            item {
                                NationalPremiumFiltersCard(
                                    isEnglish = isEnglish,

                                    availableBranches = availableBranches,
                                    selectedBranches =
                                        filters.selectedBranches,
                                    onBranchToggle = { branch ->
                                        filters = filters.copy(
                                            selectedBranches =
                                                filters.selectedBranches
                                                    .toggle(branch)
                                        )
                                    },

                                    ageGroups =
                                        NationalStatsAgeGroup.entries,
                                    selectedAgeGroups =
                                        filters.selectedAgeGroups,
                                    onAgeGroupToggle = { group ->
                                        filters = filters.copy(
                                            selectedAgeGroups =
                                                filters.selectedAgeGroups
                                                    .toggle(group)
                                        )
                                    },

                                    genders =
                                        NationalStatsGender.entries,
                                    selectedGenders =
                                        filters.selectedGenders,
                                    onGenderToggle = { gender ->
                                        filters = filters.copy(
                                            selectedGenders =
                                                filters.selectedGenders
                                                    .toggle(gender)
                                        )
                                    },

                                    availableBelts = availableBelts,
                                    selectedBelts =
                                        filters.selectedBelts,
                                    onBeltToggle = { belt ->
                                        filters = filters.copy(
                                            selectedBelts =
                                                filters.selectedBelts
                                                    .toggle(belt)
                                        )
                                    },

                                    availableGroups = availableGroups,
                                    selectedGroups =
                                        filters.selectedGroups,
                                    onGroupToggle = { group ->
                                        filters = filters.copy(
                                            selectedGroups =
                                                filters.selectedGroups
                                                    .toggle(group)
                                        )
                                    },

                                    activeOnly = filters.activeOnly,
                                    onActiveOnlyChange = { activeOnly ->
                                        filters = filters.copy(
                                            activeOnly = activeOnly
                                        )
                                    },

                                    onClearFilters = {
                                        filters =
                                            NationalStatisticsFilters()
                                    }
                                )
                            }

                            item {
                                NationalSummaryGrid(
                                    snapshot = snapshot,
                                    isEnglish = isEnglish
                                )
                            }

                            item {
                                NationalBreakdownCard(
                                    title = tr(
                                        isEnglish,
                                        "התפלגות גילאים",
                                        "Age distribution"
                                    ),
                                    icon = "🎂",
                                    values =
                                        snapshot.ageGroupCounts.mapKeys {
                                            ageGroupLabel(
                                                group = it.key,
                                                isEnglish = isEnglish
                                            )
                                        },
                                    total =
                                        snapshot.filteredUniqueTrainees,
                                    color = Color(0xFF0EA5E9)
                                )
                            }

                            item {
                                NationalBreakdownCard(
                                    title = tr(
                                        isEnglish,
                                        "התפלגות לפי מין",
                                        "Gender distribution"
                                    ),
                                    icon = "👥",
                                    values =
                                        snapshot.genderCounts.mapKeys {
                                            genderLabel(
                                                gender = it.key,
                                                isEnglish = isEnglish
                                            )
                                        },
                                    total =
                                        snapshot.filteredUniqueTrainees,
                                    color = Color(0xFF8B5CF6)
                                )
                            }

                            item {
                                NationalBreakdownCard(
                                    title = tr(
                                        isEnglish,
                                        "התפלגות חגורות",
                                        "Belt distribution"
                                    ),
                                    icon = "🥋",
                                    values =
                                        snapshot.beltCounts.mapKeys {
                                            beltLabel(
                                                belt = it.key,
                                                isEnglish = isEnglish
                                            )
                                        },
                                    total =
                                        snapshot.filteredUniqueTrainees,
                                    color = Color(0xFFF59E0B)
                                )
                            }

                            item {
                                Text(
                                    text = tr(
                                        isEnglish,
                                        "השוואה בין סניפים",
                                        "Branch comparison"
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign =
                                        startTextAlign(isEnglish),
                                    style =
                                        KmiTypography.sectionTitle.copy(
                                            fontWeight = FontWeight.Black
                                        ),
                                    color =
                                        MaterialTheme.colorScheme.onBackground,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (
                                snapshot.branchStatistics.isEmpty()
                            ) {
                                item {
                                    NationalEmptyCard(
                                        isEnglish = isEnglish
                                    )
                                }
                            } else {
                                items(
                                    items =
                                        snapshot.branchStatistics,
                                    key = { it.branchName }
                                ) { branchStats ->
                                    NationalBranchCard(
                                        statistics = branchStats,
                                        maximumTrainees =
                                            snapshot.branchStatistics
                                                .maxOfOrNull {
                                                    it.traineeCount
                                                }
                                                ?: 1,
                                        isEnglish = isEnglish
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

@Composable
private fun NationalStatisticsHero(
    snapshot: NationalStatisticsSnapshot,
    isEnglish: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF4F46E5),
                            Color(0xFF7C3AED),
                            Color(0xFF0284C7)
                        )
                    )
                )
                .padding(
                    horizontal = 18.dp,
                    vertical = 14.dp
                ),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tr(
                        isEnglish,
                        "תמונת מצב ארצית",
                        "National overview"
                    ),
                    modifier = Modifier.weight(1f),
                    style = KmiTypography.screenTitle.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = Color.White,
                    textAlign = startTextAlign(isEnglish),
                    maxLines = 2
                )
            }

            Text(
                text = tr(
                    isEnglish,
                    "נתונים מאוחדים מכל הסניפים והקבוצות",
                    "Unified data from all branches and groups"
                ),
                modifier = Modifier.fillMaxWidth(),
                style = KmiTypography.secondary,
                color = Color.White.copy(alpha = 0.84f),
                textAlign = startTextAlign(isEnglish),
                maxLines = 2
            )

            Spacer(Modifier.height(2.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                HeroPill(
                    value =
                        snapshot.filteredUniqueTrainees.toString(),
                    label = tr(
                        isEnglish,
                        "מתאמנים",
                        "Trainees"
                    ),
                    modifier = Modifier.weight(1f)
                )

                HeroPill(
                    value = snapshot.branchCount.toString(),
                    label = tr(
                        isEnglish,
                        "סניפים",
                        "Branches"
                    ),
                    modifier = Modifier.weight(1f)
                )

                HeroPill(
                    value = snapshot.groupCount.toString(),
                    label = tr(
                        isEnglish,
                        "קבוצות",
                        "Groups"
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroPill(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.20f)
        ),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 7.dp,
                vertical = 7.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = KmiTypography.metric.copy(
                    fontWeight = FontWeight.Black
                ),
                color = Color.White,
                maxLines = 1
            )

            Text(
                text = label,
                style = KmiTypography.caption,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NationalStatisticsSearch(
    query: String,
    isEnglish: Boolean,
    onQueryChange: (String) -> Unit
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(20.dp),
        placeholder = {
            Text(
                tr(
                    isEnglish,
                    "חיפוש מתאמן, סניף, קבוצה או חגורה",
                    "Search trainee, branch, group or belt"
                )
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
        },
        trailingIcon = {
            if (query.isNotBlank()) {
                IconButton(
                    onClick = {
                        onQueryChange("")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = tr(
                            isEnglish,
                            "נקה חיפוש",
                            "Clear search"
                        )
                    )
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor =
                MaterialTheme.colorScheme.surface,
            unfocusedContainerColor =
                MaterialTheme.colorScheme.surface,
            focusedBorderColor =
                MaterialTheme.colorScheme.primary,
            unfocusedBorderColor =
                MaterialTheme.colorScheme.outlineVariant,
            focusedTextColor =
                MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor =
                MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor =
                MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor =
                MaterialTheme.colorScheme.onSurfaceVariant,
            focusedLeadingIconColor =
                MaterialTheme.colorScheme.primary,
            unfocusedLeadingIconColor =
                MaterialTheme.colorScheme.onSurfaceVariant,
            focusedTrailingIconColor =
                MaterialTheme.colorScheme.primary,
            unfocusedTrailingIconColor =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}

@Composable
private fun NationalPremiumFiltersCard(
    isEnglish: Boolean,

    availableBranches: List<String>,
    selectedBranches: Set<String>,
    onBranchToggle: (String) -> Unit,

    ageGroups: List<NationalStatsAgeGroup>,
    selectedAgeGroups: Set<NationalStatsAgeGroup>,
    onAgeGroupToggle: (NationalStatsAgeGroup) -> Unit,

    genders: List<NationalStatsGender>,
    selectedGenders: Set<NationalStatsGender>,
    onGenderToggle: (NationalStatsGender) -> Unit,

    availableBelts: List<String>,
    selectedBelts: Set<String>,
    onBeltToggle: (String) -> Unit,

    availableGroups: List<String>,
    selectedGroups: Set<String>,
    onGroupToggle: (String) -> Unit,

    activeOnly: Boolean,
    onActiveOnlyChange: (Boolean) -> Unit,

    onClearFilters: () -> Unit
) {
    val activeFiltersCount =
        listOf(
            selectedBranches.isNotEmpty(),
            selectedAgeGroups.isNotEmpty(),
            selectedGenders.isNotEmpty(),
            selectedBelts.isNotEmpty(),
            selectedGroups.isNotEmpty(),
            activeOnly
        ).count { it }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 16.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {

            // =====================================================
            // כותרת
            // =====================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = tr(
                            isEnglish,
                            "סינון נתונים",
                            "Filter data"
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        style = KmiTypography.sectionTitle.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign =
                            startTextAlign(isEnglish),
                        maxLines = 2
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = tr(
                            isEnglish,
                            "התאמה מדויקת של הנתונים",
                            "Fine-tune the displayed data"
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        style = KmiTypography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign =
                            startTextAlign(isEnglish),
                        maxLines = 2
                    )
                }

                if (activeFiltersCount > 0) {
                    Spacer(Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color =
                            MaterialTheme.colorScheme.primary
                                .copy(alpha = 0.10f),
                        border = BorderStroke(
                            width = 1.dp,
                            color =
                                MaterialTheme.colorScheme.primary
                                    .copy(alpha = 0.20f)
                        ),
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 6.dp
                            ),
                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Default.Check,
                                contentDescription = null,
                                tint =
                                    MaterialTheme.colorScheme.primary,
                                modifier =
                                    Modifier.size(KmiIconSize.small)
                            )

                            Spacer(Modifier.width(4.dp))

                            Text(
                                text = tr(
                                    isEnglish,
                                    "$activeFiltersCount פעילים",
                                    "$activeFiltersCount active"
                                ),
                                style = KmiTypography.caption.copy(
                                    fontWeight =
                                        FontWeight.ExtraBold
                                ),
                                color =
                                    MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // =====================================================
            // שורה 1 — סניף + קבוצה
            // =====================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    NationalFilterSection(
                        title = tr(
                            isEnglish,
                            "📍 סניף",
                            "📍 Branch"
                        ),
                        isEnglish = isEnglish,
                        options = availableBranches,
                        selected = selectedBranches,
                        labelForOption = { branch ->
                            branch
                        },
                        onToggle = onBranchToggle
                    )
                }

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    NationalFilterSection(
                        title = tr(
                            isEnglish,
                            "👥 קבוצה",
                            "👥 Group"
                        ),
                        isEnglish = isEnglish,
                        options = availableGroups,
                        selected = selectedGroups,
                        labelForOption = { group ->
                            group
                        },
                        onToggle = onGroupToggle
                    )
                }
            }

            // =====================================================
            // שורה 2 — גיל + מין
            // =====================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    NationalFilterSection(
                        title = tr(
                            isEnglish,
                            "🎂 גיל",
                            "🎂 Age"
                        ),
                        isEnglish = isEnglish,
                        options = ageGroups,
                        selected = selectedAgeGroups,
                        labelForOption = { group ->
                            ageGroupLabel(
                                group = group,
                                isEnglish = isEnglish
                            )
                        },
                        onToggle = onAgeGroupToggle
                    )
                }

                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    NationalFilterSection(
                        title = tr(
                            isEnglish,
                            "👤 מין",
                            "👤 Gender"
                        ),
                        isEnglish = isEnglish,
                        options = genders,
                        selected = selectedGenders,
                        labelForOption = { gender ->
                            genderLabel(
                                gender = gender,
                                isEnglish = isEnglish
                            )
                        },
                        onToggle = onGenderToggle
                    )
                }
            }

            // =====================================================
            // שורה 3 — חגורה ברוחב מלא
            // =====================================================

            NationalFilterSection(
                title = tr(
                    isEnglish,
                    "🥋 חגורה",
                    "🥋 Belt"
                ),
                isEnglish = isEnglish,
                options = availableBelts,
                selected = selectedBelts,
                labelForOption = { belt ->
                    beltLabel(
                        belt = belt,
                        isEnglish = isEnglish
                    )
                },
                onToggle = onBeltToggle
            )

            // =====================================================
            // פעילים בלבד + איפוס
            // =====================================================

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment =
                    Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = activeOnly,
                    onClick = {
                        onActiveOnlyChange(!activeOnly)
                    },
                    label = {
                        Text(
                            text = tr(
                                isEnglish,
                                "פעילים בלבד",
                                "Active only"
                            ),
                            fontWeight = FontWeight.Bold
                        )
                    }
                )

                Spacer(Modifier.weight(1f))

                if (activeFiltersCount > 0) {
                    TextButton(
                        onClick = onClearFilters,
                        modifier = Modifier.heightIn(min = 56.dp)
                    ) {
                        Text(
                            text = tr(
                                isEnglish,
                                "↻ איפוס סינונים",
                                "↻ Reset filters"
                            ),
                            style = KmiTypography.action.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> NationalFilterSection(
    title: String,
    isEnglish: Boolean,
    options: List<T>,
    selected: Set<T>,
    labelForOption: (T) -> String,
    onToggle: (T) -> Unit
) {
    if (options.isEmpty()) return

    var expanded by remember {
        mutableStateOf(false)
    }

    val selectedText = remember(
        selected,
        options,
        isEnglish
    ) {
        when {
            selected.isEmpty() ->
                tr(
                    isEnglish,
                    "הכול",
                    "All"
                )

            selected.size == 1 ->
                selected.firstOrNull()
                    ?.let(labelForOption)
                    .orEmpty()

            else ->
                tr(
                    isEnglish,
                    "${selected.size} אפשרויות נבחרו",
                    "${selected.size} options selected"
                )
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement =
            Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            style = KmiTypography.caption.copy(
                fontWeight = FontWeight.ExtraBold
            ),
            color =
                MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign =
                startTextAlign(isEnglish),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Surface(
                onClick = {
                    expanded = true
                },
                modifier = Modifier.fillMaxWidth(),
                color =
                    MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(17.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color =
                        if (selected.isNotEmpty()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        }
                ),
                shadowElevation = 0.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(
                            horizontal = 11.dp,
                            vertical = 7.dp
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedText,
                        modifier = Modifier.weight(1f),
                        style = KmiTypography.body.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color =
                            if (selected.isNotEmpty()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        textAlign =
                            startTextAlign(isEnglish),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.width(6.dp))

                    Surface(
                        modifier = Modifier.size(30.dp),
                        shape = CircleShape,
                        color =
                            if (expanded) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary
                                    .copy(alpha = 0.10f)
                            },
                        shadowElevation = 0.dp,
                        tonalElevation = 0.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint =
                                    if (expanded) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                            )
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = {
                    expanded = false
                },
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 380.dp)
                    .background(
                        MaterialTheme.colorScheme.surface
                    )
            ) {
                if (selected.isNotEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = tr(
                                    isEnglish,
                                    "נקה בחירה והצג הכול",
                                    "Clear selection and show all"
                                ),
                                modifier = Modifier.fillMaxWidth(),
                                style = KmiTypography.body.copy(
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color =
                                    MaterialTheme.colorScheme.error,
                                textAlign =
                                    startTextAlign(isEnglish)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint =
                                    MaterialTheme.colorScheme.error
                            )
                        },
                        onClick = {
                            selected
                                .toList()
                                .forEach { option ->
                                    onToggle(option)
                                }

                            expanded = false
                        }
                    )
                }

                options.forEach { option ->
                    val isSelected = option in selected

                    DropdownMenuItem(
                        text = {
                            Text(
                                text = labelForOption(option),
                                modifier = Modifier.fillMaxWidth(),
                                style = KmiTypography.body.copy(
                                    fontWeight =
                                        if (isSelected) {
                                            FontWeight.ExtraBold
                                        } else {
                                            FontWeight.Medium
                                        }
                                ),
                                color =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                textAlign =
                                    startTextAlign(isEnglish),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Surface(
                                modifier = Modifier.size(24.dp),
                                shape = RoundedCornerShape(7.dp),
                                color =
                                    if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        Color.Transparent
                                    },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color =
                                        if (isSelected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        }
                                ),
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier =
                                            Modifier.fillMaxSize(),
                                        contentAlignment =
                                            Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector =
                                                Icons.Default.Check,
                                            contentDescription = null,
                                            tint =
                                                MaterialTheme.colorScheme.onPrimary,
                                            modifier =
                                                Modifier.size(KmiIconSize.small)
                                        )
                                    }
                                }
                            }
                        },
                        onClick = {
                            /*
                             * התפריט נשאר פתוח כדי לאפשר
                             * בחירה של כמה ערכים ברצף.
                             */
                            onToggle(option)
                        }
                    )
                }

                if (selected.isNotEmpty()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = tr(
                                    isEnglish,
                                    "סיום בחירה",
                                    "Done"
                                ),
                                modifier =
                                    Modifier.fillMaxWidth(),
                                style =
                                    KmiTypography.action.copy(
                                        fontWeight =
                                            FontWeight.Black
                                    ),
                                color =
                                    MaterialTheme.colorScheme.onPrimary,
                                textAlign = TextAlign.Center
                            )
                        },
                        onClick = {
                            expanded = false
                        },
                        modifier = Modifier
                            .padding(
                                horizontal = 8.dp,
                                vertical = 4.dp
                            )
                            .background(
                                color =
                                    MaterialTheme.colorScheme.primary,
                                shape =
                                    RoundedCornerShape(13.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun NationalSummaryGrid(
    snapshot: NationalStatisticsSnapshot,
    isEnglish: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            SummaryTile(
                icon = "🎂",
                title = tr(
                    isEnglish,
                    "גיל ממוצע",
                    "Average age"
                ),
                value =
                    snapshot.averageAge?.toString() ?: "—",
                accent = Color(0xFF0284C7),
                modifier = Modifier.weight(1f)
            )

            SummaryTile(
                icon = "🥋",
                title = tr(
                    isEnglish,
                    "סוגי חגורות",
                    "Belt types"
                ),
                value =
                    snapshot.beltCounts.size.toString(),
                accent = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(10.dp)
        ) {
            SummaryTile(
                icon = "⏱",
                title = tr(
                    isEnglish,
                    "ותק ממוצע",
                    "Avg seniority"
                ),
                value = snapshot.averageSeniorityYears
                    ?.let {
                        String.format(
                            Locale.US,
                            "%.1f",
                            it
                        )
                    }
                    ?: "—",
                accent = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f)
            )

            SummaryTile(
                icon = "✅",
                title = tr(
                    isEnglish,
                    "נוכחות ממוצעת",
                    "Avg attendance"
                ),
                value = snapshot.averageAttendance
                    ?.let { "$it%" }
                    ?: "—",
                accent = Color(0xFF16A34A),
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryTile(
    icon: String,
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.heightIn(
            min = 112.dp
        ),
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            accent.copy(alpha = 0.22f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = icon,
                style = KmiTypography.action
            )

            Text(
                text = value,
                style = KmiTypography.metric.copy(
                    fontWeight = FontWeight.Black
                ),
                color = accent,
                maxLines = 1
            )

            Text(
                text = title,
                style = KmiTypography.caption.copy(
                    fontWeight = FontWeight.Bold
                ),
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun NationalBreakdownCard(
    title: String,
    icon: String,
    values: Map<String, Int>,
    total: Int,
    color: Color
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            color.copy(alpha = 0.22f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = icon,
                    style = KmiTypography.action
                )

                Spacer(Modifier.width(9.dp))

                Text(
                    text = title,
                    style = KmiTypography.sectionTitle.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (values.isEmpty()) {
                Text(
                    text = "—",
                    color = Color(0xFF94A3B8)
                )
            } else {
                values.forEach { (label, count) ->
                    val progress = if (total > 0) {
                        count.toFloat() / total.toFloat()
                    } else {
                        0f
                    }

                    Column(
                        verticalArrangement =
                            Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.weight(1f),
                                style = KmiTypography.body.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "$count",
                                fontWeight = FontWeight.Black,
                                color = color
                            )
                        }

                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp),
                            color = color,
                            trackColor =
                                color.copy(alpha = 0.13f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NationalBranchCard(
    statistics: NationalBranchStatistics,
    maximumTrainees: Int,
    isEnglish: Boolean
) {
    val progress = if (maximumTrainees > 0) {
        statistics.traineeCount.toFloat() /
                maximumTrainees.toFloat()
    } else {
        0f
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement =
                Arrangement.spacedBy(11.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(42.dp),
                    shape = CircleShape,
                    color =
                        MaterialTheme.colorScheme.primaryContainer,
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📍",
                            style = KmiTypography.action
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statistics.branchName,
                        style = KmiTypography.cardTitle.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )

                    Text(
                        text = tr(
                            isEnglish,
                            "${statistics.traineeCount} מתאמנים",
                            "${statistics.traineeCount} trainees"
                        ),
                        style = KmiTypography.caption,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text =
                        statistics.traineeCount.toString(),
                    style = KmiTypography.metric.copy(
                        fontWeight = FontWeight.Black
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color =
                    MaterialTheme.colorScheme.primary,
                trackColor =
                    MaterialTheme.colorScheme.primary
                        .copy(alpha = 0.12f)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                BranchMiniStat(
                    label = tr(
                        isEnglish,
                        "גיל ממוצע",
                        "Avg age"
                    ),
                    value =
                        statistics.averageAge?.toString() ?: "—",
                    modifier = Modifier.weight(1f)
                )

                BranchMiniStat(
                    label = tr(
                        isEnglish,
                        "נוכחות",
                        "Attendance"
                    ),
                    value = statistics.averageAttendance
                        ?.let { "$it%" }
                        ?: "—",
                    modifier = Modifier.weight(1f)
                )

                BranchMiniStat(
                    label = tr(
                        isEnglish,
                        "חגורות",
                        "Belts"
                    ),
                    value =
                        statistics.beltCounts.size.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun BranchMiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 7.dp,
                vertical = 8.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = KmiTypography.metric.copy(
                    fontWeight = FontWeight.Black
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = label,
                style = KmiTypography.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun NationalStatisticsLoading(
    isEnglish: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        KmiLoadingRings(
            text = tr(
                isEnglish,
                "טוען נתונים מכל הסניפים...",
                "Loading data from all branches..."
            )
        )
    }
}

@Composable
private fun NationalStatisticsError(
    message: String,
    isEnglish: Boolean,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(28.dp),
        horizontalAlignment =
            Alignment.CenterHorizontally,
        verticalArrangement =
            Arrangement.Center
    ) {
        Text(
            text = "⚠️",
            style = KmiTypography.screenTitle
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = message,
            modifier = Modifier.fillMaxWidth(),
            style = KmiTypography.body,
            textAlign = TextAlign.Center,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        TextButton(
            onClick = onRetry,
            modifier = Modifier.heightIn(min = 56.dp)
        ) {
            Text(
                text = tr(
                    isEnglish,
                    "נסה שוב",
                    "Try again"
                ),
                style = KmiTypography.action.copy(
                    fontWeight = FontWeight.Black
                )
            )
        }
    }
}

@Composable
private fun NationalEmptyCard(
    isEnglish: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            width = 1.dp,
            color =
                MaterialTheme.colorScheme.outlineVariant
        ),
        shadowElevation = 0.dp,
        tonalElevation = 0.dp
    ) {
        Text(
            text = tr(
                isEnglish,
                "לא נמצאו נתונים המתאימים למסננים.",
                "No data matches the selected filters."
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(22.dp),
            style = KmiTypography.body.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center,
            color =
                MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun <T> Set<T>.toggle(value: T): Set<T> {
    return if (value in this) {
        this - value
    } else {
        this + value
    }
}

private fun tr(
    isEnglish: Boolean,
    he: String,
    en: String
): String {
    return if (isEnglish) en else he
}

private fun startTextAlign(
    isEnglish: Boolean
): TextAlign {
    return if (isEnglish) {
        TextAlign.Left
    } else {
        TextAlign.Right
    }
}

private fun genderLabel(
    gender: NationalStatsGender,
    isEnglish: Boolean
): String {
    return when (gender) {
        NationalStatsGender.MALE ->
            tr(isEnglish, "זכר", "Male")

        NationalStatsGender.FEMALE ->
            tr(isEnglish, "נקבה", "Female")

        NationalStatsGender.OTHER ->
            tr(isEnglish, "אחר", "Other")

        NationalStatsGender.UNKNOWN ->
            tr(isEnglish, "לא צוין", "Not specified")
    }
}

private fun ageGroupLabel(
    group: NationalStatsAgeGroup,
    isEnglish: Boolean
): String {
    return when (group) {
        NationalStatsAgeGroup.CHILDREN ->
            tr(isEnglish, "ילדים 0–11", "Children 0–11")

        NationalStatsAgeGroup.TEENS ->
            tr(isEnglish, "נוער 12–17", "Teens 12–17")

        NationalStatsAgeGroup.YOUNG_ADULTS ->
            tr(isEnglish, "צעירים 18–25", "Young adults 18–25")

        NationalStatsAgeGroup.ADULTS ->
            tr(isEnglish, "בוגרים 26–40", "Adults 26–40")

        NationalStatsAgeGroup.MATURE_ADULTS ->
            tr(isEnglish, "בוגרים 41–59", "Adults 41–59")

        NationalStatsAgeGroup.SENIORS ->
            tr(isEnglish, "גיל 60 ומעלה", "Age 60+")

        NationalStatsAgeGroup.UNKNOWN ->
            tr(isEnglish, "גיל לא ידוע", "Unknown age")
    }
}

private fun beltLabel(
    belt: String,
    isEnglish: Boolean
): String {
    if (!isEnglish) return belt

    return when (belt.trim()) {
        "לבנה" -> "White"
        "צהובה" -> "Yellow"
        "כתומה" -> "Orange"
        "ירוקה" -> "Green"
        "כחולה" -> "Blue"
        "חומה" -> "Brown"
        "שחורה" -> "Black"
        "ללא דרגה" -> "No rank"
        else -> belt
    }
}

/*
 * ============================================================
 * PDF — סטטיסטיקה ארצית
 * ============================================================
 */

private fun shareNationalStatisticsPdf(
    context: Context,
    snapshot: NationalStatisticsSnapshot,
    filters: NationalStatisticsFilters,
    isEnglish: Boolean
) {
    val pdfFile = createNationalStatisticsPdf(
        context = context,
        snapshot = snapshot,
        filters = filters,
        isEnglish = isEnglish
    )

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        pdfFile
    )

    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"

        putExtra(
            Intent.EXTRA_SUBJECT,
            if (isEnglish) {
                "KAMI National Statistics"
            } else {
                "סטטיסטיקה ארצית - KAMI"
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
            sendIntent,
            if (isEnglish) {
                "Share PDF"
            } else {
                "שיתוף PDF"
            }
        )
    )
}

private fun createNationalStatisticsPdf(
    context: Context,
    snapshot: NationalStatisticsSnapshot,
    filters: NationalStatisticsFilters,
    isEnglish: Boolean
): File {

    val pageWidth = 595
    val pageHeight = 842

    val margin = 30f
    val contentWidth =
        pageWidth.toFloat() - margin * 2f

    val document = PdfDocument()

    val navy =
        AndroidColor.rgb(
            2,
            43,
            74
        )

    val blue =
        AndroidColor.rgb(
            36,
            103,
            158
        )

    val lightBlue =
        AndroidColor.rgb(
            234,
            246,
            255
        )

    val borderBlue =
        AndroidColor.rgb(
            191,
            213,
            232
        )

    val textDark =
        AndroidColor.rgb(
            15,
            23,
            42
        )

    val textMuted =
        AndroidColor.rgb(
            80,
            100,
            120
        )

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

    fun paint(
        size: Float,
        color: Int = textDark,
        typeface: Typeface = regularTypeface,
        align: Paint.Align =
            if (isEnglish) {
                Paint.Align.LEFT
            } else {
                Paint.Align.RIGHT
            }
    ): Paint {
        return Paint(
            Paint.ANTI_ALIAS_FLAG
        ).apply {
            textSize = size
            this.color = color
            this.typeface = typeface
            textAlign = align
        }
    }

    val sectionPaint = paint(
        size = 16f,
        color = blue,
        typeface = boldTypeface
    )

    val labelPaint = paint(
        size = 10f,
        color = textMuted,
        typeface = boldTypeface
    )

    val valuePaint = paint(
        size = 18f,
        color = navy,
        typeface = boldTypeface
    )

    val bodyPaint = paint(
        size = 10f,
        color = textDark
    )

    val bodyBoldPaint = paint(
        size = 10f,
        color = textDark,
        typeface = boldTypeface
    )

    val smallPaint = paint(
        size = 8.5f,
        color = textMuted
    )

    fun textXStart(): Float {
        return if (isEnglish) {
            margin
        } else {
            pageWidth.toFloat() - margin
        }
    }

    fun drawRoundedRect(
        canvas: AndroidCanvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        radius: Float = 10f
    ) {
        val rectPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.FILL
            }

        canvas.drawRoundRect(
            left,
            top,
            right,
            bottom,
            radius,
            radius,
            rectPaint
        )
    }

    fun drawRoundedBorder(
        canvas: AndroidCanvas,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        color: Int,
        radius: Float = 10f
    ) {
        val rectPaint =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.color = color
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

        canvas.drawRoundRect(
            left,
            top,
            right,
            bottom,
            radius,
            radius,
            rectPaint
        )
    }

    var pageNumber = 0
    var page: PdfDocument.Page? = null
    var canvas: AndroidCanvas? = null
    var y = 0f

    fun newPage() {
        page?.let {
            document.finishPage(it)
        }

        pageNumber++

        val pageInfo =
            PdfDocument.PageInfo.Builder(
                pageWidth,
                pageHeight,
                pageNumber
            ).create()

        val newPage =
            document.startPage(pageInfo)

        page = newPage
        canvas = newPage.canvas

        /*
         * ============================================================
         * Header — זהה ל-PDF של מסך הבית
         * ============================================================
         */

        val pageCanvas = newPage.canvas

        pageCanvas.drawColor(
            AndroidColor.WHITE
        )

val headerBottom = 122f

val navyPaint =
    Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color = navy
        style = Paint.Style.FILL
    }

val accent1 =
    Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color =
            AndroidColor.rgb(
                36,
                103,
                158
            )
        style = Paint.Style.FILL
    }

val accent2 =
    Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color =
            AndroidColor.rgb(
                128,
                183,
                220
            )
        style = Paint.Style.FILL
    }

/*
 * האלכסון הראשי.
 */
        pageCanvas.drawPath(
    AndroidPath().apply {
        moveTo(
            pageWidth.toFloat(),
            0f
        )
        lineTo(
            pageWidth.toFloat(),
            headerBottom
        )
        lineTo(
            178f,
            headerBottom
        )
        lineTo(
            238f,
            0f
        )
        close()
    },
    navyPaint
)

/*
 * פס אקסנט ראשון.
 */
        pageCanvas.drawPath(
    AndroidPath().apply {
        moveTo(
            208f,
            headerBottom
        )
        lineTo(
            224f,
            headerBottom
        )
        lineTo(
            284f,
            0f
        )
        lineTo(
            268f,
            0f
        )
        close()
    },
    accent1
)

/*
 * פס אקסנט שני.
 */
        pageCanvas.drawPath(
    AndroidPath().apply {
        moveTo(
            230f,
            headerBottom
        )
        lineTo(
            238f,
            headerBottom
        )
        lineTo(
            298f,
            0f
        )
        lineTo(
            290f,
            0f
        )
        close()
    },
    accent2
)

/*
 * לוגו KAMI.
 */
val logoX = 78f
val logoY = 58f
val logoRadius = 42f

        pageCanvas.drawCircle(
    logoX,
    logoY,
    logoRadius,
    navyPaint
)

        pageCanvas.drawCircle(
    logoX,
    logoY,
    logoRadius - 4f,
    Paint(
        Paint.ANTI_ALIAS_FLAG
    ).apply {
        color =
            AndroidColor.WHITE
    }
)

        pageCanvas.drawText(
    "KAMI",
    logoX,
    logoY + logoRadius * 0.22f,
    paint(
        size = logoRadius * 0.62f,
        color = navy,
        typeface = boldTypeface,
        align = Paint.Align.CENTER
    )
)

val headerX =
    pageWidth - 34f

/*
 * כותרת.
 */
        pageCanvas.drawText(
    if (isEnglish) {
        "National Statistics"
    } else {
        "סטטיסטיקה ארצית"
    },
    headerX,
    52f,
    paint(
        size = 25f,
        color =
            AndroidColor.WHITE,
        typeface = boldTypeface,
        align = Paint.Align.RIGHT
    )
)

/*
 * תת־כותרת.
 */
        pageCanvas.drawText(
    if (isEnglish) {
        "KAMI national overview"
    } else {
        "תמונת מצב ארצית"
    },
    headerX,
    78f,
    paint(
        size = 11f,
        color =
            AndroidColor.WHITE,
        typeface = regularTypeface,
        align = Paint.Align.RIGHT
    )
)

/*
 * תאריך ההפקה מתחת לכותרת,
 * כמו בדוח מסך הבית.
 */
val generatedDate =
    SimpleDateFormat(
        "dd/MM/yyyy",
        Locale.getDefault()
    ).format(
        Date()
    )

        pageCanvas.drawText(
            if (isEnglish) {
                "Generated: $generatedDate"
            } else {
                "תאריך הפקה: $generatedDate"
            },
            headerX,
            142f,
            paint(
                size = 8.5f,
                color = textMuted,
                typeface = regularTypeface,
                align = Paint.Align.RIGHT
            )
        )

        /*
         * מספר עמוד קבוע בתחתית כל עמוד.
         */
        pageCanvas.drawText(
            if (isEnglish) {
                "Page $pageNumber"
            } else {
                "עמוד $pageNumber"
            },
            pageWidth / 2f,
            pageHeight - 18f,
            paint(
                size = 8.5f,
                color = textMuted,
                typeface = regularTypeface,
                align = Paint.Align.CENTER
            )
        )

        /*
         * מתחילים את תוכן הדוח מתחת לתאריך.
         */
        y = 164f
    }

    fun ensureSpace(
        requiredHeight: Float
    ) {
        if (
            y + requiredHeight >
            pageHeight - 35f
        ) {
            newPage()
        }
    }

    /*
     * מחזיר תמיד את ה-Canvas של העמוד הפעיל.
     * newPage() חייב להיקרא לפני כל ציור ראשון,
     * ולכן מצב null כאן הוא מצב לא תקין.
     */
    fun currentCanvas(): AndroidCanvas {
        return requireNotNull(canvas) {
            "PDF canvas is not initialized"
        }
    }

    fun drawSectionTitle(
        title: String
    ) {
        ensureSpace(35f)

        currentCanvas().drawText(
            title,
            textXStart(),
            y,
            sectionPaint
        )

        y += 20f
    }

    fun drawMetricCard(
        label: String,
        value: String,
        left: Float,
        top: Float,
        width: Float
    ) {
        val right = left + width
        val bottom = top + 62f

        val activeCanvas =
            currentCanvas()

        drawRoundedRect(
            canvas = activeCanvas,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            color = lightBlue
        )

        drawRoundedBorder(
            canvas = activeCanvas,
            left = left,
            top = top,
            right = right,
            bottom = bottom,
            color = borderBlue
        )

        val centerX =
            left + width / 2f

        val centeredValuePaint =
            Paint(valuePaint).apply {
                textAlign =
                    Paint.Align.CENTER
            }

        val centeredLabelPaint =
            Paint(labelPaint).apply {
                textAlign =
                    Paint.Align.CENTER
            }

        activeCanvas.drawText(
            value,
            centerX,
            top + 28f,
            centeredValuePaint
        )

        activeCanvas.drawText(
            label,
            centerX,
            top + 47f,
            centeredLabelPaint
        )
    }

    fun drawBreakdown(
        title: String,
        values: List<Pair<String, Int>>
    ) {
        drawSectionTitle(title)

        if (values.isEmpty()) {
            currentCanvas().drawText(
                "—",
                textXStart(),
                y,
                bodyPaint
            )

            y += 20f
            return
        }

        values.forEach { (label, count) ->
            /*
             * ensureSpace() עלולה ליצור עמוד חדש,
             * לכן מקבלים את ה-Canvas רק אחריה.
             */
            ensureSpace(22f)

            val line =
                if (isEnglish) {
                    "$label: $count"
                } else {
                    "$label : $count"
                }

            currentCanvas().drawText(
                line,
                textXStart(),
                y,
                bodyPaint
            )

            y += 17f
        }

        y += 8f
    }

    /*
     * ========================================================
     * עמוד ראשון
     * ========================================================
     */

    newPage()

    drawSectionTitle(
        if (isEnglish) {
            "National overview"
        } else {
            "תמונת מצב ארצית"
        }
    )

    val cardGap = 8f
    val cardWidth =
        (contentWidth - cardGap * 2f) / 3f

    val secondCardLeft =
        margin + cardWidth + cardGap
    val thirdCardLeft =
        margin + (cardWidth + cardGap) * 2f

    drawMetricCard(
        label =
            if (isEnglish) {
                "Trainees"
            } else {
                "מתאמנים"
            },
        value =
            snapshot.filteredUniqueTrainees
                .toString(),
        left = margin,
        top = y,
        width = cardWidth
    )

    drawMetricCard(
        label =
            if (isEnglish) {
                "Branches"
            } else {
                "סניפים"
            },
        value =
            snapshot.branchCount
                .toString(),
        left = secondCardLeft,
        top = y,
        width = cardWidth
    )

    drawMetricCard(
        label =
            if (isEnglish) {
                "Groups"
            } else {
                "קבוצות"
            },
        value =
            snapshot.groupCount
                .toString(),
        left = thirdCardLeft,
        top = y,
        width = cardWidth
    )

    y += 78f

    /*
     * ארבעת הנתונים שמופיעים בכרטיסי
     * הסיכום במסך.
     */

    val twoCardWidth =
        (contentWidth - cardGap) / 2f

    drawMetricCard(
        label =
            if (isEnglish) {
                "Average age"
            } else {
                "גיל ממוצע"
            },
        value =
            snapshot.averageAge
                ?.toString()
                ?: "—",
        left = margin,
        top = y,
        width = twoCardWidth
    )

    drawMetricCard(
        label =
            if (isEnglish) {
                "Belt types"
            } else {
                "סוגי חגורות"
            },
        value =
            snapshot.beltCounts.size
                .toString(),
        left =
            margin +
                    twoCardWidth +
                    cardGap,
        top = y,
        width = twoCardWidth
    )

    y += 72f

    drawMetricCard(
        label =
            if (isEnglish) {
                "Avg seniority"
            } else {
                "ותק ממוצע"
            },
        value =
            snapshot.averageSeniorityYears
                ?.let {
                    String.format(
                        Locale.US,
                        "%.1f",
                        it
                    )
                }
                ?: "—",
        left = margin,
        top = y,
        width = twoCardWidth
    )

    drawMetricCard(
        label =
            if (isEnglish) {
                "Avg attendance"
            } else {
                "נוכחות ממוצעת"
            },
        value =
            snapshot.averageAttendance
                ?.let { "$it%" }
                ?: "—",
        left =
            margin +
                    twoCardWidth +
                    cardGap,
        top = y,
        width = twoCardWidth
    )

    y += 82f

    /*
     * ========================================================
     * מסננים פעילים
     * ========================================================
     */

    drawSectionTitle(
        if (isEnglish) {
            "Active filters"
        } else {
            "מסננים פעילים"
        }
    )

    val filterLines =
        mutableListOf<String>()

    if (filters.searchQuery.isNotBlank()) {
        filterLines +=
            if (isEnglish) {
                "Search: ${filters.searchQuery}"
            } else {
                "חיפוש: ${filters.searchQuery}"
            }
    }

    if (filters.selectedBranches.isNotEmpty()) {
        filterLines +=
            if (isEnglish) {
                "Branches: " +
                        filters.selectedBranches
                            .joinToString(", ")
            } else {
                "סניפים: " +
                        filters.selectedBranches
                            .joinToString(", ")
            }
    }

    if (filters.selectedGroups.isNotEmpty()) {
        filterLines +=
            if (isEnglish) {
                "Groups: " +
                        filters.selectedGroups
                            .joinToString(", ")
            } else {
                "קבוצות: " +
                        filters.selectedGroups
                            .joinToString(", ")
            }
    }

    if (filters.selectedBelts.isNotEmpty()) {
        filterLines +=
            if (isEnglish) {
                "Belts: " +
                        filters.selectedBelts
                            .joinToString(", ") {
                                beltLabel(
                                    belt = it,
                                    isEnglish = true
                                )
                            }
            } else {
                "חגורות: " +
                        filters.selectedBelts
                            .joinToString(", ")
            }
    }

    if (filters.selectedGenders.isNotEmpty()) {
        filterLines +=
            if (isEnglish) {
                "Gender: " +
                        filters.selectedGenders
                            .joinToString(", ") {
                                genderLabel(
                                    gender = it,
                                    isEnglish = true
                                )
                            }
            } else {
                "מין: " +
                        filters.selectedGenders
                            .joinToString(", ") {
                                genderLabel(
                                    gender = it,
                                    isEnglish = false
                                )
                            }
            }
    }

    if (filters.selectedAgeGroups.isNotEmpty()) {
        filterLines +=
            if (isEnglish) {
                "Age: " +
                        filters.selectedAgeGroups
                            .joinToString(", ") {
                                ageGroupLabel(
                                    group = it,
                                    isEnglish = true
                                )
                            }
            } else {
                "גיל: " +
                        filters.selectedAgeGroups
                            .joinToString(", ") {
                                ageGroupLabel(
                                    group = it,
                                    isEnglish = false
                                )
                            }
            }
    }

    if (filters.activeOnly) {
        filterLines +=
            if (isEnglish) {
                "Active trainees only"
            } else {
                "מתאמנים פעילים בלבד"
            }
    }

    if (filterLines.isEmpty()) {
        currentCanvas().drawText(
            if (isEnglish) {
                "No filters"
            } else {
                "ללא סינון"
            },
            textXStart(),
            y,
            bodyPaint
        )

        y += 22f
    } else {
        filterLines.forEach { line ->
            ensureSpace(20f)

            /*
             * ensureSpace() יכולה לפתוח עמוד חדש,
             * לכן משתמשים ב-Canvas המעודכן.
             */
            currentCanvas().drawText(
                line,
                textXStart(),
                y,
                smallPaint
            )

            y += 15f
        }

        y += 10f
    }

    /*
     * ========================================================
     * התפלגויות
     * ========================================================
     */

    drawBreakdown(
        title =
            if (isEnglish) {
                "Age distribution"
            } else {
                "התפלגות גילאים"
            },
        values =
            snapshot.ageGroupCounts
                .map { (group, count) ->
                    ageGroupLabel(
                        group = group,
                        isEnglish = isEnglish
                    ) to count
                }
    )

    drawBreakdown(
        title =
            if (isEnglish) {
                "Gender distribution"
            } else {
                "התפלגות לפי מין"
            },
        values =
            snapshot.genderCounts
                .map { (gender, count) ->
                    genderLabel(
                        gender = gender,
                        isEnglish = isEnglish
                    ) to count
                }
    )

    drawBreakdown(
        title =
            if (isEnglish) {
                "Belt distribution"
            } else {
                "התפלגות חגורות"
            },
        values =
            snapshot.beltCounts
                .toList()
                .sortedBy {
                    beltOrder(it.first)
                }
                .map { (belt, count) ->
                    beltLabel(
                        belt = belt,
                        isEnglish = isEnglish
                    ) to count
                }
    )

    /*
     * ========================================================
     * השוואת סניפים
     * ========================================================
     */

    drawSectionTitle(
        if (isEnglish) {
            "Branch comparison"
        } else {
            "השוואה בין סניפים"
        }
    )

    if (snapshot.branchStatistics.isEmpty()) {
        currentCanvas().drawText(
            if (isEnglish) {
                "No branch data"
            } else {
                "אין נתוני סניפים"
            },
            textXStart(),
            y,
            bodyPaint
        )

        y += 20f
    } else {
        snapshot.branchStatistics
            .forEach { branch ->

                /*
                 * ייתכן שמעבר עמוד התרחש כאן,
                 * ולכן מקבלים Canvas חדש רק לאחר ensureSpace().
                 */
                ensureSpace(58f)

                val activeCanvas =
                    currentCanvas()

                val cardTop = y - 12f
                val cardBottom =
                    cardTop + 50f

                drawRoundedRect(
                    canvas = activeCanvas,
                    left = margin,
                    top = cardTop,
                    right =
                        pageWidth.toFloat() -
                                margin,
                    bottom = cardBottom,
                    color =
                        AndroidColor.rgb(
                            247,
                            250,
                            253
                        )
                )

                drawRoundedBorder(
                    canvas = activeCanvas,
                    left = margin,
                    top = cardTop,
                    right =
                        pageWidth.toFloat() -
                                margin,
                    bottom = cardBottom,
                    color = borderBlue
                )

                activeCanvas.drawText(
                    branch.branchName,
                    textXStart(),
                    y + 3f,
                    bodyBoldPaint
                )

                y += 17f

                val details =
                    if (isEnglish) {
                        "${branch.traineeCount} trainees" +
                                "  |  Avg age: " +
                                (
                                        branch.averageAge
                                            ?.toString()
                                            ?: "—"
                                        ) +
                                "  |  Attendance: " +
                                (
                                        branch.averageAttendance
                                            ?.let { "$it%" }
                                            ?: "—"
                                        ) +
                                "  |  Belts: " +
                                branch.beltCounts.size
                    } else {
                        "${branch.traineeCount} מתאמנים" +
                                "  |  גיל ממוצע: " +
                                (
                                        branch.averageAge
                                            ?.toString()
                                            ?: "—"
                                        ) +
                                "  |  נוכחות: " +
                                (
                                        branch.averageAttendance
                                            ?.let { "$it%" }
                                            ?: "—"
                                        ) +
                                "  |  חגורות: " +
                                branch.beltCounts.size
                    }

                activeCanvas.drawText(
                    details,
                    textXStart(),
                    y + 3f,
                    smallPaint
                )

                y += 34f
            }
    }

    /*
     * סוגרים את העמוד האחרון.
     */
    page?.let {
        document.finishPage(it)
    }

    val pdfDirectory =
        File(
            context.cacheDir,
            "shared_pdfs"
        ).apply {
            mkdirs()
        }

    val fileName =
        if (isEnglish) {
            "National Statistics.pdf"
        } else {
            "סטטיסטיקה ארצית.pdf"
        }

    val pdfFile =
        File(
            pdfDirectory,
            fileName
        )

    FileOutputStream(pdfFile, false).use {
        document.writeTo(it)
    }

    document.close()

    return pdfFile
}

private fun beltOrder(belt: String): Int {
    return when (belt.trim()) {
        "לבנה" -> 0
        "צהובה" -> 1
        "כתומה" -> 2
        "ירוקה" -> 3
        "כחולה" -> 4
        "חומה" -> 5
        "שחורה" -> 6
        else -> 7
    }
}