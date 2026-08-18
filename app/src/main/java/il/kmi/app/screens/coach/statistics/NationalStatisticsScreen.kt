package il.kmi.app.screens.coach.statistics

import android.app.Activity
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.sp
import il.kmi.app.ui.KmiTopBar
import il.kmi.shared.localization.AppLanguage
import il.kmi.shared.localization.AppLanguageManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NationalStatisticsScreen(
    isEnglish: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false,
    onOpenDrawer: () -> Unit = {
        il.kmi.app.ui.DrawerBridge.open()
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

    val availableBranches = remember(allRecords) {
        allRecords
            .flatMap { it.branches }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    val availableGroups = remember(allRecords) {
        allRecords
            .flatMap { it.groups }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
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
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFF8FBFF),
                                Color(0xFFEAF4FF),
                                Color(0xFFDDEBFA)
                            )
                        )
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
                            modifier = Modifier.fillMaxSize(),
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
                                    textAlign = startTextAlign(isEnglish),
                                    style =
                                        MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF0F172A)
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
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 9.dp
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
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text(
                text = "📊",
                fontSize = 28.sp
            )

            Text(
                text = tr(
                    isEnglish,
                    "תמונת מצב ארצית",
                    "National overview"
                ),
                fontSize = 27.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = tr(
                    isEnglish,
                    "נתונים מאוחדים מכל הסניפים והקבוצות",
                    "Unified data from all branches and groups"
                ),
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.84f)
            )

            Spacer(Modifier.height(5.dp))

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(10.dp)
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
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 10.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontSize = 23.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )

            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1
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
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = Color(0xFF7C3AED),
            unfocusedBorderColor = Color(0xFFD7DEEA)
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
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFDDD6FE)
        ),
        shadowElevation = 5.dp
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
                        fontSize = 20.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF172036),
                        textAlign =
                            startTextAlign(isEnglish)
                    )

                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = tr(
                            isEnglish,
                            "התאמה מדויקת של הנתונים",
                            "Fine-tune the displayed data"
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign =
                            startTextAlign(isEnglish)
                    )
                }

                if (activeFiltersCount > 0) {
                    Spacer(Modifier.width(8.dp))

                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color =
                            Color(0xFF7C3AED)
                                .copy(alpha = 0.10f),
                        border = BorderStroke(
                            width = 1.dp,
                            color =
                                Color(0xFF7C3AED)
                                    .copy(alpha = 0.20f)
                        )
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
                                tint = Color(0xFF7C3AED),
                                modifier =
                                    Modifier.size(15.dp)
                            )

                            Spacer(Modifier.width(4.dp))

                            Text(
                                text = tr(
                                    isEnglish,
                                    "$activeFiltersCount פעילים",
                                    "$activeFiltersCount active"
                                ),
                                fontSize = 11.sp,
                                fontWeight =
                                    FontWeight.ExtraBold,
                                color = Color(0xFF6D28D9)
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
                        onClick = onClearFilters
                    ) {
                        Text(
                            text = tr(
                                isEnglish,
                                "↻ איפוס סינונים",
                                "↻ Reset filters"
                            ),
                            fontWeight =
                                FontWeight.ExtraBold,
                            color = Color(0xFF6D28D9)
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
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF475569),
            maxLines = 1,
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
                color = Color.White,
                shape = RoundedCornerShape(17.dp),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (selected.isNotEmpty()) {
                        Color(0xFF7C3AED)
                    } else {
                        Color(0xFFD7DEEA)
                    }
                ),
                shadowElevation = if (expanded) {
                    5.dp
                } else {
                    1.dp
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
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
                        fontSize = 13.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected.isNotEmpty()) {
                            Color(0xFF6D28D9)
                        } else {
                            Color(0xFF475569)
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(Modifier.width(6.dp))

                    Surface(
                        modifier = Modifier.size(30.dp),
                        shape = CircleShape,
                        color = if (expanded) {
                            Color(0xFF7C3AED)
                        } else {
                            Color(0xFF7C3AED).copy(
                                alpha = 0.10f
                            )
                        }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector =
                                    Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = if (expanded) {
                                    Color.White
                                } else {
                                    Color(0xFF7C3AED)
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
                    .background(Color.White)
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
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFDC2626)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = Color(0xFFDC2626)
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
                                fontWeight = if (isSelected) {
                                    FontWeight.ExtraBold
                                } else {
                                    FontWeight.Medium
                                },
                                color = if (isSelected) {
                                    Color(0xFF6D28D9)
                                } else {
                                    Color(0xFF334155)
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        leadingIcon = {
                            Surface(
                                modifier = Modifier.size(24.dp),
                                shape = RoundedCornerShape(7.dp),
                                color = if (isSelected) {
                                    Color(0xFF7C3AED)
                                } else {
                                    Color.Transparent
                                },
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) {
                                        Color(0xFF7C3AED)
                                    } else {
                                        Color(0xFFCBD5E1)
                                    }
                                )
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
                                            tint = Color.White,
                                            modifier =
                                                Modifier.size(16.dp)
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
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Black,
                                color = Color.White
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
                                color = Color(0xFF7C3AED),
                                shape = RoundedCornerShape(13.dp)
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun NationalActiveFilterCard(
    activeOnly: Boolean,
    hasFilters: Boolean,
    isEnglish: Boolean,
    onActiveOnlyChange: (Boolean) -> Unit,
    onClearFilters: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            1.dp,
            Color(0xFFDDE4EF)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = activeOnly,
                onClick = {
                    onActiveOnlyChange(!activeOnly)
                },
                label = {
                    Text(
                        tr(
                            isEnglish,
                            "פעילים בלבד",
                            "Active only"
                        )
                    )
                }
            )

            Spacer(Modifier.weight(1f))

            if (hasFilters) {
                TextButton(onClick = onClearFilters) {
                    Text(
                        tr(
                            isEnglish,
                            "ניקוי מסננים",
                            "Clear filters"
                        ),
                        fontWeight = FontWeight.Bold
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
        modifier = modifier.height(112.dp),
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        border = BorderStroke(
            1.dp,
            accent.copy(alpha = 0.16f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement =
                Arrangement.SpaceBetween
        ) {
            Text(
                text = icon,
                fontSize = 22.sp
            )

            Text(
                text = value,
                fontSize = 27.sp,
                fontWeight = FontWeight.Black,
                color = accent
            )

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                maxLines = 1
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
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp,
        border = BorderStroke(
            1.dp,
            color.copy(alpha = 0.16f)
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
                    fontSize = 23.sp
                )

                Spacer(Modifier.width(9.dp))

                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF0F172A)
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
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF334155)
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
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 4.dp,
        border = BorderStroke(
            1.dp,
            Color(0xFFDDE5F0)
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
                    color = Color(0xFF4F46E5)
                        .copy(alpha = 0.12f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📍",
                            fontSize = 20.sp
                        )
                    }
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = statistics.branchName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )

                    Text(
                        text = tr(
                            isEnglish,
                            "${statistics.traineeCount} מתאמנים",
                            "${statistics.traineeCount} trainees"
                        ),
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                Text(
                    text =
                        statistics.traineeCount.toString(),
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF4F46E5)
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = Color(0xFF4F46E5),
                trackColor =
                    Color(0xFF4F46E5).copy(alpha = 0.12f)
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
        color = Color(0xFFF5F7FC)
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
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF334155)
            )

            Text(
                text = label,
                fontSize = 10.sp,
                color = Color(0xFF64748B),
                maxLines = 1
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = Color(0xFF7C3AED)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = tr(
                isEnglish,
                "טוען נתונים מכל הסניפים...",
                "Loading data from all branches..."
            ),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569)
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠️",
            fontSize = 38.sp
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = message,
            textAlign = TextAlign.Center,
            color = Color(0xFF475569),
            lineHeight = 21.sp
        )

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = onRetry) {
            Text(
                tr(
                    isEnglish,
                    "נסה שוב",
                    "Try again"
                ),
                fontWeight = FontWeight.Black
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
        color = Color.White,
        shape = RoundedCornerShape(22.dp)
    ) {
        Text(
            text = tr(
                isEnglish,
                "לא נמצאו נתונים המתאימים למסננים.",
                "No data matches the selected filters."
            ),
            modifier = Modifier.padding(22.dp),
            textAlign = TextAlign.Center,
            color = Color(0xFF64748B)
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